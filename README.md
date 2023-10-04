# A local proxy for Forex rates

The goal of this coding exercise was to build Forex, a local proxy for getting currency exchange rates. The source of
the rates is [One-Frame service](https://hub.docker.com/r/paidyinc/one-frame), which has a quota of 1000 requests per
day. The requirement for the Forex API is to support at least 10000 requests per day, and the provided rates must not be
older than 5 minutes.

## Assumptions

In the process of completing this task, the following assumptions were made:

* One-Frame service is the only available source of the exchange rate information
* The only way to get an exchange rate is to copy it from the `price` field of the One-Frame service response. It's not
  possible to calculate one exchange rate based on others, e. g., rate for USDJPY can't be used to calculate a rate for
  JPYUSD, and it's not possible to calculate USDGBP based on USDJPY and JPYGBP.
* We must support all the currencies supported by the One-Frame service. Per service documentation, the list can be
  found [here](https://www.xe.com/iso4217.php), and it contains 162 currencies.
* Other than stated quotas, there is no additional requirements regarding usage of networking and compute resources.
* Forex is a standalone application which runs on a single machine. There is no need to support distributed state and
  load balancing.

## Calculations

162 currencies give us 162 * 161 = 26082 currency pairs, which mean that if we use a naive strategy of mirroring the
Forex requests to the One-Frame service, one by one, it would be possible to immediately exhaust the quota of 1000
requests by requesting 1001 exchange rates for different currency pairs, thus failing the requirement to support at
least 10000 requests.

Fortunately, there's a difference in the APIs that could be exploited, namely, the One-Frame service supports multiple
currency pairs per request, while Forex supports only one. Thus, it's possible to reduce the number of requests to the
One-Frame service by requesting not only the currency pair which is being asked for, but also a maximum possible amount
of arbitrary currency pairs, which can then be stored and served without a need to waste One-Frame request quota for the
next 5 minutes.

It turns out that the maximum number of currency pairs per request in One-Frame is 339, anything longer is rejected
because of limitations of the URI length. With the described caching strategy, it allows us to get all the currency
pairs in 26082 / 339 ≈ 77 requests. Unfortunately, that's still not enough. The cache entries expire in 5 minutes, which
means that in the worst case the quota will be exhausted in 1000 / 77 ≈ 13 5-minute intervals, or a little over a hour.
This is the limit of the caching approach, and with the assumptions stated above, I don't think it's possible to improve
upon it. Improving the worst case will need a change in requirements, either in expiry interval, number of supported
currencies and/or a request quota for the Forex service.

We can, however, still improve the average case to make the best effort to support the maximum possible number of
requests. Some improvements can be made by assuming that not all currency pairs are distributed equally, and some will
be requested more often than others. By keeping track of request statistics and always making sure that the popular
currency pairs are cached first, it's possible to maximize the number of cache hits and keep the usage of One-Frame
service to the minimum.

## Implementation

Most of the new functionality was added by implementing two services, `OneFrameLive` service which implements the
One-Frame API protocol for getting currency exchange rates, and `RateCacheService` which stores the retrieved currency
rates and keeps track of the usage statistics. Most of the business logic regarding the usage of these services is kept
in `RatesProgram`. The cache is implemented by storing the `Deferred` objects in
a `ConcurrentHashMap`. `ConcurrentHashMap` provides atomicity of storing, comparing and retrieving the cache
entries. `Deferred` guarantees that only one thread is performing requests to the One-Frame service, while the others
are waiting.

The cache keeps the number of requests per entry, realizing the Least-Frequently-Used cache eviction strategy, except no
entries are actually evicted, as it's assumed that we're not restricted by RAM. In addition, a score decay factor is
applied hourly to make it possible to support changes in access patterns. The factor is set up to make each score point
lose half of its value in a week. 
