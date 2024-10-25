JAVAC = javac
JAR = jar
JAVA_FILES = $(shell find src -name "*.java")
CLASS_FILES = $(JAVA_FILES:.java=.class)
TARGET = target/classes
JAR_TARGET = target/chordprotocol.jar
MANIFEST = manifest.txt
LOG_DIR = logs

NODE_COUNT ?= 10
BIT_LENGTH ?= 10

all: $(JAR_TARGET)

$(JAR_TARGET): $(CLASS_FILES)
	mkdir -p target
	$(JAR) -cfm $(JAR_TARGET) $(MANIFEST) -C $(TARGET) .

%.class: %.java
	mkdir -p $(TARGET)
	$(JAVAC) -d $(TARGET) -sourcepath src $<

clean:
	rm -rf $(TARGET) $(JAR_TARGET) $(LOG_DIR)

run: $(JAR_TARGET)
	java -cp $(JAR_TARGET) Simulator $(NODE_COUNT) $(BIT_LENGTH)

rerun: clean all run

sim: $(JAR_TARGET)
	@mkdir -p $(LOG_DIR)
	make run NODE_COUNT=10 BIT_LENGTH=10 | tee $(LOG_DIR)/sim-10_10.log
	make run NODE_COUNT=100 BIT_LENGTH=20 | tee $(LOG_DIR)/sim-100_20.log
	make run NODE_COUNT=1000 BIT_LENGTH=20 | tee $(LOG_DIR)/sim-1000_20.log

cleansim: clean all sim
