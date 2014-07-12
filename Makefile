all: build run

build:
	mvn clean package | grep -v "skipping" | grep -v "Download"
	chmod +x target/aie_btc_service-*-SNAPSHOT-jar-with-dependencies.jar

run:
	mvn clean package exec:java -Dexec.mainClass=io.aie_btc_service.aie_btc_service.BTCService | grep -v "skipping"

runb:
	mvn clean package exec:java -Dexec.mainClass=io.aie_btc_service.aie_btc_service.API1Service | grep -v "skipping"
