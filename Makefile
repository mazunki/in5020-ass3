JAVAC = javac
JAR = jar
JAVA_FLAGS = -Xlint:unchecked

JAVA_FILES = $(shell find src/main/java -name "*.java")
CLASS_FILES = $(JAVA_FILES:.java=.class)
TARGET = target/classes
BINDIR = bin
JAR_TARGET = $(BINDIR)/chordprotocol.jar
MANIFEST = src/resources/manifest.txt
LOG_DIR = logs

NODE_COUNT ?= 10
BIT_LENGTH ?= 10

all: $(JAR_TARGET)

$(JAR_TARGET): $(CLASS_FILES)
	@mkdir -p target
	$(JAR) -cfm $(JAR_TARGET) $(MANIFEST) -C $(TARGET) .

%.class: %.java
	@mkdir -p $(TARGET)
	$(JAVAC) $(JAVA_FLAGS) -d $(TARGET) -sourcepath src/main/java $<

clean:
	rm -rf $(TARGET) $(JAR_TARGET) $(LOG_DIR)

run:
	java -cp $(JAR_TARGET) com.ass3.Simulator $(NODE_COUNT) $(BIT_LENGTH)

rerun: clean all run

case1:
	make run NODE_COUNT=10 BIT_LENGTH=10

case2:
	make run NODE_COUNT=100 BIT_LENGTH=20

case3:
	make run NODE_COUNT=1000 BIT_LENGTH=20

sim: $(JAR_TARGET)
	mkdir -p $(LOG_DIR)
	make run case1 | tee $(LOG_DIR)/sim-100_20.log
	make run case2 | tee $(LOG_DIR)/sim-100_20.log
	make run case3 | tee $(LOG_DIR)/sim-1000_20.log

