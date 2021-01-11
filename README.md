# SAP JCo RFC
![GitHub repo size](https://img.shields.io/github/repo-size/itertop/sap-jco-rfc)

SAP JCo RFC is a simple command line tool written in Java that allows you to test connection to SAP NW ABAP Application Server and call SAP RFC enabled Function Modules with with SAP Java Connector (SAP JCo).
## Prerequisites
Before you begin, ensure you have met the following requirements:
* You are running Unix-like Operating System natively or in a Docker container to follow this readme. Should work on Windows but not tested.
* You have JDK/JRE 5, 6, 7, 8 or 11. 
* You have installed [SAP Java Connector 3](https://support.sap.com/en/product/connectors/jco.html).

java -classpath /opt/sapbc48/server/packages/SAP/code/jars/static/sapjco3.jar -Djava.library.path=/opt/sapbc48/server/packages/SAP/code/libs com.sap.conn.jco.rt.About -stdout

## Installing SAP JCo RFC
### Checking prerequisites
#### Java
Make sure you have JDK installed by calling ```java -version```.
The right output looks like this:
```bash
[root]# java -version
openjdk version "1.8.0_275"
OpenJDK Runtime Environment (build 1.8.0_275-b01)
OpenJDK 64-Bit Server VM (build 25.275-b01, mixed mode)
```
The response contains `64-bit Server VM` referring to 64-bit JVM,  the output of 32-bit JVM contains `Client VM`. It's important to align the bitness of your JVM with SAP JCo.

If you got a response like ```bash: java: command not found```, then install JDK of your choice, OpenJDK 8 will do (`yum install java-1.8.0-openjdk-devel` or `apt`, `apk`, `dnf`, `pacman`, `brew` or whatever package manager you are using instead of `yum`).

#### SAP Java Connector 
Check SAP Jco installation
```bash
[root]# java -classpath $CLASSPATH  -Djava.library.path=$LD_LIBRARY_PATH com.sap.conn.jco.rt.About -stdout | less
```
Here it's assumed that `$CLASSPATH` points to the `sapjco3.jar` and `$LD_LIBRARY_PATH` to directory containing `sapjco3.jar`, scroll down to the bottom of this section for the details.
The correct output should start like this:
```bash
[root]# java -classpath $CLASSPATH  -Djava.library.path=$LD_LIBRARY_PATH com.sap.conn.jco.rt.About -stdout
--------------------------------------------------------------------------------------
|                                 SAP Java Connector                                 |
|                Copyright (c) 2000-2020 SAP SE. All rights reserved.                |
|                                Version Information                                 |
--------------------------------------------------------------------------------------
Java Runtime:
 Operating System:    Linux 4.19.121-linuxkit for amd64
 Java VM:             1.8.0_275 Red Hat, Inc.
 Default charset:     US-ASCII
Versions:
 JCo API:             3.1.3 (2020-09-18)
 JCo middleware:      JavaRfc 2.10.1
 JCo library:         753.710
Library Paths:
 JCo archive:         /opt/sap/lib/sapjco3.jar
 JCo library:         /opt/sap/lib/libsapjco3.so
```

If SAP JCo is not installed, then you need to download it from [SAP Download portal](https://support.sap.com/en/product/connectors/jco.html) (SAP S/C/I/D-User required), consideing  the bitness of your JVM and machine processor. Currently, the latest version is 3.1.3, shipped as a file `sapjco31P_3-70004517.zip` containing a gzipped tar file `sapjco3-linuxx86_64-3.1.3.tgz` which is to be unzipped to the directory of your chioce (e.g. `/opt/sap/lib` as in the above output), further referred to as `<sapjco_install_dir>`.
The delivery contains samples, documentation, license info and the connector itself as two library files:  `sapjco3.jar` and `libsapjco3.so` (`.sl` for HP-UX). 

Once unpacked and copied to the `<sapjco_install_dir>`, set the below environmental variables or, even better, add them to your `~/.bashrc` (or `~/.zshrc`,` ~/.shrc`, `~/.kshrc`, `~/.cshrc` or whatever shell you are using), as follows:

```bash
echo LD_LIBRARY_PATH=<sapjco_install_dir> >> ~/.bashrc &&
echo CLASSPATH=<sapjco_install_dir>/sapjco3.jar >> ~/.bashrc &&
source ~/.bashrc
```
Please note if you have these variables set in your system (`echo $CLASSPATH` is not empty), then add `$CLASSPATH:` after = to preserve your current setup.

### Installation
The program uses Apache Maven to manage build and import the lastest [PicoCLI](https://picocli.info) as a nice Java command-line interface, so the fastest way will be to install `maven` and `git` to clone this repo.
```
yum install -y maven git-all
```

Download the repo.
```
git clone git://github.com/itertop/sap-jco-rfc.git
```
Build the tool
```
cd sap-jco-rfc
mvn package
```
It will take some time at the first time, at the end you should get something like that:
```bash
[INFO] Building jar: /root/sap-jco-rfc/target/sap-jco-rfc-jar-with-dependencies.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 47.717 s
[INFO] Finished at: 2021-01-11T00:20:55Z
[INFO] ------------------------------------------------------------------------
[root@fb70dc155d2b sap-jco-rfc]#
```
Successfull build indicates creation of `target/sap-jco-rfc-jar-with-dependencies.jar`, which is ready to run as `java -cp "$CLASSPATH:./target/sap-jco-rfc-jar-with-dependencies.jar" com.company.JCo` with the desired command line parameters (see below Using section).

To make a command line shorter, create an alias in your `~/.bashrc`:
```
alias sapjcorfc='java -cp "$CLASSPATH:/path/to/sap-jco-rfc-jar-with-dependencies.jar" com.company.JCo'
```

## Using SAP JCo RFC
The program has three working modes:
1. Connection test: specify only connection parameters with `sapjcorfc -pc <connection_credentials>`.
2. Testing existence of a remote function module on SAP side and get the list of its parameters: specify `sapjcorfc -pc <connection_credentials> <rfc_module>`.
3. Calling remote FM with parameters (note that tables and structures are not supported): specify `sapjcorfc -pc <connection_credentials> -pi <import_parameters> -pch <changing_parameters> <rfc_module>`.

For example:
1. Connection test.
 * for user/pass connection: 
 ```
 sapjcorfc -pcon ashost=icpxxxx.wdf.sap.corp -pc sysnr=00 -pc client=800 -pc user=ddic -pc passwd=********
 ```
note putting backslash in front of special symbols (e.g. \$ for $).
 * for snc connection: 
 ```
sapjcorfc -pc ashost=icpxxxx.wdf.sap.corp -pc sysnr=00 -pc client=800 -pc snc_mode=1 -pc snc_partnername=p:CN=Cxxxxxxx,O=SAP-AG,C=DE -pc client.snc_lib=/home/oracle/sec/libsapcrypto.so
 ```
Successful connection gives the following output:
```
~/dev/sap-jco-rfc> sapjcorfc -pc=ashost=liner.itertop.com -pc=sysnr=00 -pc=client=800 -pc=user=itertop -pc=passwd=\$R\$V4Lfv
Connecting to SAP System..
Attributes:
DEST:                  SAPSystem
OWN_HOST:              mbp-av
PARTNER_HOST:          liner
PARTNER_IP:            172.17.0.2
SYSTNR:                00
SYSID:                 ITT
CLIENT:                800
USER:                  ITERTOP
LANGUAGE:              E
ISO_LANGUAGE:          EN
OWN_CODEPAGE:          4102
OWN_CHARSET:           UTF16
OWN_ENCODING:          utf-16
OWN_BYTES_PER_CHAR:    2
PARTNER_CODEPAGE:      4103
PARTNER_CHARSET:       UTF16
PARTNER_ENCODING:      utf-16
PARNER_BYTES_PER_CHAR: 2
OWN_REL:               753
PARTNER_REL:           750
PARTNER_TYPE:          3
KERNEL_REL:            753
TRACE:
RFC_ROLE:              C
OWN_TYPE:              E
CPIC_CONVID:           00000000
```

You are free to specify any connection properties `jco.client.XXX` supported by SAP simply specifying them as `-pc XXX=<value>`.
If any issues, connector the tool throws a JCoException with the meaningful response, like a below one for missing user name:
```
Connecting to SAP System..
Attributes:
com.sap.conn.jco.JCoException: (101) JCO_ERROR_CONFIGURATION: Initialization of destination SAPSystem failed: No user identity is configured
``` 
2. Testing FM.

There's a simple FM `STFC_CHANGING` that receives an import parameter `START_VALUE` and changing parameter `COUNTER`, returns `RESULT` as a sum of `START_VALUE` and `COUNTER` and increments `COUNTER` by 1:   

```ABAP
FUNCTION STFC_CHANGING.
*"----------------------------------------------------------------------
*"*"Lokale Schnittstelle:
*"  IMPORTING
*"     VALUE(START_VALUE) TYPE  I
*"  EXPORTING
*"     VALUE(RESULT) TYPE  I
*"  CHANGING
*"     VALUE(COUNTER) TYPE  I
*"----------------------------------------------------------------
RESULT = START_VALUE + COUNTER.
COUNTER = COUNTER + 1.
ENDFUNCTION.
```
Let's try to examine it with our tool:
```
sapjcorfc -pc=ashost=liner.itertop.com -pc=sysnr=00 -pc=client=800 -pc=user=itertop -pc=passwd=\$RFV4rfv STFC_CHANGING
```
gives the following output:
```
Checking FM 'STFC_CHANGING'..
Module found in the remote system.
Getting import parameters:
	name: START_VALUE, type: INT, optional: false, value: 0
Getting changing parameters:
	name: COUNTER, type: INT, optional: false, value: 0

Setting parameters:

Executing FM 'STFC_CHANGING'..

Success.
Getting export parameters:
	name: RESULT, type: INT, optional: true, value: 0
Getting changed parameters:
	name: COUNTER, type: INT, optional: false, value: 1
```
3. Finally, let's pass a value and get a return.
```bash
~/dev/sap-jco-rfc > sapjcorfc -pc=ashost=liner.itertop.com -pc=sysnr=00 -pc=client=800 -pc=user=itertop -pc=passwd=\$RFV4rfv -pi START_VALUE=10 -pch COUNTER=5 STFC_CHANGING
```
 gives the following output:

```
Initializing parameter START_VALUE with 10
Initializing parameter COUNTER with 5

Executing FM 'STFC_CHANGING'..

Success.
Getting export parameters:
	name: RESULT, type: INT, optional: true, value: 15
Getting changed parameters:
	name: COUNTER, type: INT, optional: false, value: 6
```

## Contributing to SAP JCo RFC
You are more than welcome to contribute to the project. 
If you'd like to, here are some ideas as an example:
* It would be great to add Windows support (most likely this is more about just testing and updating readme). 
* Adding structures and tables as RFC parameters as current version supports values only.
* Such a sophisticated and complex development direction as supporting SAP Java IDoc Library which is an add-on for the SAP Jco.

To contribute to SAP JCo RFC, follow these steps:

1. Fork this repository.
2. Create a branch: `git checkout -b <branch_name>`.
3. Make your changes and commit them: `git commit -m '<commit_message>'`
4. Push to the original branch: `git push origin <project_name>/<location>`
5. Create the pull request.

Alternatively see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

## Contact
If you want to contact me you can reach me at <alex@itertop.com>.

Files created:
dev_jco_rfc.log
SAPSystem.JCO destination
