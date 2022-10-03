FROM docker.io/library/eclipse-temurin:17

WORKDIR /root

RUN /bin/sh -c set -eux; \
    apt-get update -y; \
    apt-get install -y --no-install-recommends \
            wget \
            unzip;\
    rm -rf /var/lib/apt/lists/*; \
    wget --progress dot:giga https://download.eclipse.org/ee4j/glassfish/glassfish-6.2.5.zip; \
    unzip glassfish-6.2.5.zip; \
    rm glassfish-6.2.5.zip

RUN echo "export SHELL=/bin/bash" >> "$HOME"/.bashrc

ENV GLASSFISH_HOME=/root/glassfish6
ENV CLASSPATH=.:$GLASSFISH_HOME/glassfish/lib/*:$GLASSFISH_HOME/glassfish/lib/appserv-rt.jar:$GLASSFISH_HOME/glassfish/lib/javaee.jar
ENV DERBY_HOME=$GLASSFISH_HOME/javadb
ENV PATH=$PATH:$GLASSFISH_HOME/glassfish/bin:$DERBY_HOME/bin

SHELL ["/bin/bash", "-c"]

EXPOSE 1527/tcp 4848/tcp 8080/tcp 8181/tcp

CMD ["/bin/bash"]
