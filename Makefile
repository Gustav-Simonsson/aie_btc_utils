all: build run

build:
	mvn clean package
	chmod +x target/aie_btc_service-*-SNAPSHOT-jar-with-dependencies.jar

run:
	java -jar target/aie_btc_service-*-SNAPSHOT-jar-with-dependencies.jar
