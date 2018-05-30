# Distributed System Project
All code is written by me, please contact me if you want to make any revision or academic use.

========================================================
Distributed Systems
COMP90015 2017 SM1
Project 1 - EZShare
Resource Sharing Network
Introduction
In Project 1 we will build a resource sharing network that consists of servers, which can communicate with
each other, and clients which can communicate with the servers. The system will be called EZShare.
In typical usage, each user that wants to share files will start an EZShare server on the machine that contains
the files. An EZShare client can be used to instruct the server to share the files.
Servers can be queried for what files they are sharing. Clients can request a shared file be downloaded to
them.
Servers can connect to other servers and queries can propogate throughout all of the servers.
In general, servers can publish resources; a file is just one kind of resource. In EZShare, other resources are
just references (URIs) to e.g. web pages.
Every published resource (including shared files) has an optional owner and channel to which it belongs.
These things allow resources to be controlled, e.g. not all shared resources have to be available to the public.

=========================================================
Distributed Systems
COMP90015 2017 SM1
Project 2 - Security and Subscribing
Project 2
The project involves building on Project 1. If you have not satisfactorily completed Project 1 then you'll need
to work with your tutor and lecturer to catch up.
You are required to:
Implement a secure socket port that works along side the existing port. This will include making
secure connections during exchange and query operations between servers.
·
Implement a SUBSCRIBE/UNSUBSCRIBE command, that will receive published/shared resource
descriptions as they are published/shared.
·
For the security aspect you will make use of certificates and secure sockets as discussed in the lectures.



==========================Project 1============================
Arguments for Server

-debug

-port 8070 -debug


================================================================
Some arguments for Client:

-exchange -servers localhost:8070 -debug

1.publish-------------------------------------------------------

-publish -name "Baidu" -description "A search tool" -uri http://www.baidu.com -debug

-publish -name "Unimelb website" -description "The main page for the University of Melbourne" -uri http://www.unimelb.edu.au -debug

-publish -owner "frankie" -description "MUST website" -uri http://www.must.edu.mo -debug

2.remove and query----------------------------------------------

-publish -uri http://www.must.edu.mo -debug

-remove -owner "frankie" -uri http://www.must.edu.mo -debug

-query -uri http://www.must.edu.mo -debug

-query -debug

3.exchange (port 8080 with 8070)---------------------------------

-exchange -servers localhost:8070,sunrise.cis.unimelb.edu.au:3780 -debug

-query -debug

4.share and fetch (from local server)-----------------------------

-share -name "frankiephoto" -description "A photo of Frankie" -uri file:///C:/Users/frankie/Desktop/test.jpg -tags jpg -secret whoami -debug

-fetch -uri file:///C:/Users/frankie/Desktop/test.jpg -debug

5.query and fetch from a remote server-----------------------------

-query -host "sunrise.cis.unimelb.edu.au" -port 3780 -debug

-fetch -host "sunrise.cis.unimelb.edu.au" -port 3780 -uri file:///usr/local/share/ezshare/photo.jpg -debug




=======================project 2=====================


-publish -owner "frankie" -description "MUST website" -uri http://www.must.edu.mo -debug -secure

-subscribe -debug
