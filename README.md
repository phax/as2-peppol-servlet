#as2-peppol-servlet

A stand alone servlet that takes AS2 requests with OpenPEPPOL StandardBusinessDocuments and handles them via SPI. This is not a self-contained package, but a good starting point for handling PEPPOL AS2 messages.

An example application that uses *as2-peppol-servlet* for receiving PEPPOL AS2 messages is my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project. It may serve as a practical starting point.

This package depends on **[ph-commons](https://github.com/phax/ph-commons)**, **[ph-sbdh](https://github.com/phax/ph-sbdh)**, **[as2-lib and as2-servlet](https://github.com/phax/as2-lib)**. This transitively includes Bouncy Castle (1.53) and javax.mail (1.5.4) among other libraries.

*as2-peppol-servlet* handles incoming AS2 messages, and parses them as OASIS Standard Business Documents (SBD). It does not contain extraction of the SBD content or even handling of the UBL content since the purpose of this project is reusability. For validating the SBD against PEPPOL rules, the project **[peppol-sbdh](https://github.com/phax/peppol-sbdh)** is available and for handling UBL 2.0 or 2.1 files you may have a look at my **[ph-ubl](https://github.com/phax/ph-ubl)**.

Since version 3.0.0 this project uses [as2-servlet](https://github.com/phax/as2-lib). Previously the servlet code was contained in this project but I decided to extract the generic part into a separate project for easier re-use.

This project is licensed under the Apache 2 License.

Versions <= 1.0.1 are compatible with ph-commons < 6.0.
Versions >= 2.0.0 are compatible with ph-commons >= 6.0.

#Usage
To use this project you have to do the following:
  1. Configure the AS2 servlet as specified in the [as2-servlet docs](https://github.com/phax/as2-lib)
  2. The key store must contain your PEPPOL AP certificate and the alias of the only entry must be the CN-value of your certificate's subject (e.g. `APP_1000000001`).
  3. Inside your project create an SPI implementation of the `com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI` interface to handling incoming SBD documents.

##Add project via Maven
Add the following to your pom.xml to use this artifact:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-peppol-servlet</artifactId>
  <version>2.2.1</version>
</dependency>
```

##AS2 Configuration file
Additionally to the configuration file specified in as2-servlet an additional processor must be added:
 
```xml
<?xml version="1.0" encoding="utf-8"?>
<openas2>
  ...
    <!-- [required] Process incoming SBD documents -->
    <module classname="com.helger.as2servlet.sbd.AS2ServletSBDModule" />      
  </processor>
</openas2>
```

##SPI implementation

SPI stands for "Service provider interface" and is a Java standard feature to enable loose but typed coupling. [Read more on SPI](http://docs.oracle.com/javase/tutorial/ext/basics/spi.html)

A [dummy SPI implementation](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/java/com/helger/as2servlet/sbd/MockIncomingSBDHandler.java) is contained in the test code of this project. Additionally you need to create a file `META-INF/services/com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI` (in the `src/main/resources/` folder when using Maven) which contains a single line referencing the implementation class. An [example file](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/resources/META-INF/services/com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI) is located in the test resources of this project.

# Known issues

  * PEPPOL AS2 specs requires that duplicate incoming message IDs are handled specially, by ignoring multiple transmissions of the same message ID
  * The certificate check of the sender's certificate must be improved 

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
