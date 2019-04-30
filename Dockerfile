FROM alpine:latest
ENV LANG C.UTF-8
RUN apk add --no-cache wget
RUN apk add --no-cache shadow
ENV JAVA_HOME="/jdk-11"
RUN echo "Downloading jdk build"

RUN wget -Y on --no-check-certificate http://drive.jku.at/ssf/s/readFile/share/8207/4867522971216226929/publicLink/openjdk-11-GA_linux-x64-musl_bin.tar.gz

RUN echo "Downloading sha256 checksum"
RUN wget -Y on --no-check-certificate http://drive.jku.at/ssf/s/readFile/share/8208/-1932052387783488162/publicLink/openjdk-11-GA_linux-x64-musl_bin.tar.gz.sha256

ENV JDK_ARCHIVE="openjdk-11-GA_linux-x64-musl_bin.tar.gz"
RUN echo "Verify checksum"
RUN sha256sum -c ${JDK_ARCHIVE}.sha256

RUN echo "Checksum is correct"


RUN echo "Extract JDK"
RUN tar -xzf ${JDK_ARCHIVE}
RUN rm ${JDK_ARCHIVE} ${JDK_ARCHIVE}.sha256 

ENV PATH $PATH:${JAVA_HOME}/bin

ENV JAVA_VERSION 11-ea+28
ENV JAVA_ALPINE_VERSION 11~28-1

ENV HOME /home/guest
RUN mkdir -p $HOME
RUN usermod -d $HOME guest
RUN groups guest
RUN chown guest:users $HOME
WORKDIR $HOME
USER guest
RUN wget -Y on --no-check-certificate https://d3kbcqa49mib13.cloudfront.net/spark-2.2.0-bin-hadoop2.7.tgz
RUN tar xvzf spark-2.2.0-bin-hadoop2.7.tgz
RUN mv spark-2.2.0-bin-hadoop2.7 $HOME/spark
RUN rm spark-2.2.0-bin-hadoop2.7.tgz

#ENV SPARK_HOME $HOME/spark
#ENV PATH $PATH:$HOME/spark/bin
#ADD setenv.sh /home/guest/setenv.sh
#RUN echo . ./setenv.sh >> .bashrc
