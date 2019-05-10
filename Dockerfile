FROM alpine:latest
ENV LANG C.UTF-8
ENV JAVA_VERSION 8u202
ENV PYTHON_VERSION 3.6.8-r2
ENV JAVA_ALPINE_VERSION 8.212.04-r0
ENV SPARK_VERSION 2.2.0
ENV HADOOP_VERSION 2.7
ENV SPARK_HOME /home/guest/spark
RUN apk update
RUN apk add --no-cache coreutils procps shadow openssl bash openjdk8="$JAVA_ALPINE_VERSION"  python3-dev="$PYTHON_VERSION" && ln -s /usr/bin/python3 /usr/bin/python
ENV HOME /home/guest
RUN mkdir -p $HOME
RUN usermod -d $HOME guest
RUN groups guest
RUN chown guest:users $HOME
WORKDIR $HOME
USER guest
RUN wget -Y on --no-check-certificate  "https://d3kbcqa49mib13.cloudfront.net/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz"
RUN tar xvzf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz
RUN mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} $SPARK_HOME
RUN rm spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz
RUN mkdir /home/guest/ZookeeperRecoveryMode
COPY Init.sh /home/guest
COPY StartMaster.sh /home/guest
COPY StartSlave.sh /home/guest
COPY StartApplication.sh /home/guest
