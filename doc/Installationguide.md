# Installation guide

Here we show a way to install services in an wps server on an
linux operation system like ubuntu.

## Prerequisites

This installation guide is based on a provided docker image with the
installation of a wps server by 52°North.

We used the folder you can find 
[here](https://52north.org/delivery/riesgos/gfz-wps.zip).
To use the stuff you have to extract the content of a zip to a location
in which the server should run.

```
wget https://52north.org/delivery/riesgos/gfz-wps.zip
unzip gfz-wps.zip -d gfz-wps
```

If you don't have access to a docker image like this or you need to 
install the wps server as a standalone server please refer to the
[installation guide from 52°North](https://github.com/52North/WPS/wiki/Setting-up-the-52%C2%B0North-WPS-with-Ecplise).


Using the content of this folder you just need to install docker, 
docker-compose and maven before the installation process starts.


Using Ubuntu 18.10 you just need to run the following command:

```
sudo apt-get install docker-io docker-compose maven
```

You also need to install maven on your system to compile this java project.

```
sudo apt-get install maven
```

## Make sure the server can run docker

This wps service repository strongly relies on running the processes
inside of docker containers.
To archive this goal it is necessary that the server is able to execute
docker commands.

If the server runs as a standalone server it should be enough to have
docker installed and to give the executing user the privileges to
run docker commands.

If your wps server runs inside of docker itself (as it is the case in the
provided gfz-wps folder), you need to modify the Dockerfile in this
folder to install the docker binaries inside of the image.

The docker image in our case uses a debian system (the "FROM" command of the
Dockerfile points to the tomcat:9-jre8 image, 
which is based on a debian stretch image itself). 
You must add the the following lines at the end of the Dockerfile:

```
RUN apt update 
RUN apt install apt-transport-https ca-certificates curl software-properties-common gnupg2 -y
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt update 
RUN apt install docker-ce -y
```

Using this code the image will have all the binaries to execute docker
commands.

Using the wps server inside of docker you also need to edit
the docker-compose.yml file to provide access to the docker socket.
Just insert the following line in the volumes section:

```
volumes:
    [...]
    - /var/run/docker.sock:/var/run/docker.sock
```

In case you also want to have a shared folder with your own
configuration files for added services, you should also add
a volume entry for the folder where you want to put this files into.

```
volumes:
    [...]
    - ./json-configs:/usr/share/riesgos/json-configurations
```
The default path that is used to save the configurations is the
/usr/share/riesgos/json-configuration folder, but you can change this
path later. The json-configs is a folder that you can create in the
folder next to the Dockerfile. You can put json configurations
into this folder later.


Then all you have to do is to (re)create the image using docker-compose
```
docker-compose build
```
(You must be in the directory with the docker compose and the Dockerfile to
run this command.)


## Build the package

The next step is to build the jar package for this project.

To download the source code of this package just use git clone
or download the files as a zip from the github site

Once you have downloaded the code, you can go into to the root
directory of the project and run

```
mvn clean package
```

which downloads the dependencies, 
compiles the sources, 
runs the tests and builds the package.

If the command succeed you can find the jar file in the target folder.

## Copy the jar to the WEB-INF/lib folder of the server

To able the server to access the code you must copy the jar file
into the WEB-INF/lib/ folder of the server.

In case you run the server from the docker-container you can find this
folder under 52n-wps-webapp-4.0.0-beta.7-SNAPSHOT/WEB-INF/lib.

## Check java dependencies

To be able to use all of the functions of this project you must make
sure that the necessary dependencies are provided by the server.

In case you use the docker image with the 52°North server,
you have to add the following jars into the WEB-INF/lib folder:

- commons-compress-1.9.jar
- ant-1.10.5.jar

Because of the usage of maven to build the jar file, maven already
downloaded the dependencies to your computer.

In case of Ubuntu 18.10 you can find the jars under your home
directory:
```
~/.m2/repository/
```

In case of commons-compress you find the jar under:
```
~/.m2/repository/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar
```

In case you run your own standalone server just check if the jars are
inside of your WEB-INF/lib folder.
Maybe also libraries that are not explizit mentioned here necessary as
well, because we focused on running the server inside of the docker
image. This already included several other jar files.
In case you have a question about this, feel free to contact us.

## Edit the dispatcher-servlet.xml

In the normal case the server configuration file you can find 
under WEB-INF/classes/dispatcher-servlet.xml of the server.
It contains the information where which packages to scan for wps services.
At the beginning it is too restrictive for our cases.

You have to change the line
```
<context:component-scan base-package="org.n52.wps">
```
to
```
<context:component-scan base-package="org.n52">
```

(This is the necessary step as under: 
https://github.com/riesgos/52north-wps-osmtovector-process).

## Create the docker images for the supported processes

As explained in the first steps the services in this repository run
inside of docker images, so you must provide this images to be accessible
for the server.

For the predefined services we provide the Dockerfiles to create the images.

Just go into the folder
```
assistance/dockerfiles/quakeledger
```
and run

```
docker build . --tag quakeledger
```
to build the image for the quakeledger process.

To build the image for shakyground switch in the folder
```
assistance/dockerfiles/shakyground
```
and run
```
docker build . --tag shakyground
```
Especially downloading some grid data here may take a lot of time.

At the moment there is a 1:1 mapping of the processes and the docker images.
This is mostly for handling the dependencies seperate for each process.
Nothing will stop you in using one docker image for multiple processes.

## Optional configure the folder to use as a configuration repository

This project relies on providing json files as configurations for
how to call command line programs in docker images and how to handle 
input and output.
You can configure a path where the program searches for configurations
to integrate them as services.
You can find this option under the wps page of your server
(localhost:8080/wps in case you use the docker image for the server)
and find it on the Repositories (in the navigation) and under the
GFZ RIESGOS Configuration Module.
Its default location is 
```
/usr/share/riesgos/json-configurations
```
In case you added the volume line for this folder in the docker-compose.yml
file you can now add configurations in this folder to provide
access to your own algorithms.

## Start the server

In case you use the server in the docker image just go to the
folder with the docker-compose.yml file and run
```
docker-compose up
```

Now the server should start and the supported processes are included.