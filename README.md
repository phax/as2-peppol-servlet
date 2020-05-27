# as2-peppol-servlet

# This project continues on https://github.com/phax/as2-peppol

# Status migration AS2 &rarr; AS4

Peppol migrated to AS4 as the mandatory transport protocol as of February 1<sup>st</sup>, 2020.
The support of AS2 will be gracefully faded out.
Personally I wouldn't recommend to start new Peppol AS2 projects.

See **phase4** as an AS4 solution that can send and receive Peppol Messages: https://github.com/phax/phase4

# News and Noteworthy

* v5.3.1 - 2020-02-17
    * Updated to ph-web 9.1.9
* v5.3.0 - 2020-02-07
    * Fixed using the correct transport profile in receiver checks when using Peppol AS2 v2
    * Updated to peppol-commons 8.x
    * Made Peppol AS2 v2 the default
* v5.2.0 - 2019-12-19
    * Updated to as2-lib 4.5.0
* v5.1.2 - 2019-11-11
    * Updated to as2-lib 4.4.5
* v5.1.1 - 2019-10-01
    * Added default constructor to `AS2ServletSBDModule`
* v5.1.0 - 2019-09-26
    * Added support to handle headers of incoming messages - API change
    * Added support for PEPPOL AS2 profile v2
    * Updated to as2-lib 4.4.4
* v5.0.5 - 2019-06-18
    * Updated to peppol-commons 7.0.0
    * Updated to as2-lib 4.4.0
* v5.0.4 - 2019-05-17
    * Updated to as2-lib 4.3.0
* v5.0.3 - 2018-11-26
    * Requires ph-commons 9.2.0
* v5.0.2 - 2018-06-29
  * Updated to ph-commons 9.1.2
* v5.0.1 - 2018-04-06
* v5.0.0 - 2018-02-12
  * Updated to ph-commons 9.0.0
  * Updated to BouncyCastle 1.59
* v4.0.2 - 2017-06-07
  * Updated to as2-lib 3.0.3
* v4.0.1 - 2017-01-11
  * Just a new release with no new features
  * Binds to ph-commons 8.6.x
* v4.0.0 - 2016-08-22
  * Updated to JDK 8
  * Binds to ph-commons 8.4.x
* v3.0.0
  * This project uses [as2-servlet](https://github.com/phax/as2-lib). Previously the servlet code was contained in this project but I decided to extract the generic part into a separate project for easier re-use.
* v2.0.0
  * Requires ph-commons 6.x
* v1.0.1
  * Requires ph-commons 5.x

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)
