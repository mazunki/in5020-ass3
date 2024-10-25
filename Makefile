JAVAC = javac
JAR = jar
JAVA_FILES = $(shell find src -name "*.java")
CLASS_FILES = $(JAVA_FILES:.java=.class)
TARGET = target/classes
JAR_TARGET = target/chordprotocol.jar
MANIFEST = manifest.txt

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
	rm -rf $(TARGET) $(JAR_TARGET)

run:
	java -cp $(JAR_TARGET) Simulator $(NODE_COUNT) $(BIT_LENGTH)

rerun: clean all run

