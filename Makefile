all: build run

build:
	mvn clean package | grep -v "skipping"
	chmod +x target/aie_btc_service-*-SNAPSHOT-jar-with-dependencies.jar

run:
	mvn clean package exec:java -Dexec.mainClass=io.aie_btc_service.aie_btc_service.BTCService | grep -v "skipping"
