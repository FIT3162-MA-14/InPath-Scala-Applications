FROM adelineyaoy/spark-submit
  
LABEL maintainer="Adeline Chew"

ARG SBT_VERSION
ENV SBT_VERSION=${SBT_VERSION:-1.5.5}

RUN wget -O - https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | gunzip | tar -x -C /usr/local

ENV PATH /usr/local/sbt/bin:${PATH}

WORKDIR /app

# Pre-install base libraries
ADD build.sbt /app/
ADD project/plugins.sbt /app/project/
RUN sbt update

COPY template.sh /
RUN chmod +x template.sh

# ENV SPARK_APPLICATION_MAIN_CLASS Application
ENV SPARK_APPLICATION_MAIN_CLASS com.deline.chew.ML.StreamingKMeans

# Copy the build.sbt first, for separate dependency resolving and downloading
 COPY build.sbt /app/
 COPY project /app/project
 RUN sbt update

# Copy the source code and build the application
 COPY . /app
 RUN sbt clean assembly

CMD ["/template.sh"]