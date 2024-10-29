JAVAC = javac
JAR = jar
JAVA_FLAGS = -Xlint:unchecked

LOG_DIR = logs/
TARGET_DIR = target
SRC_DIR = src/main/java
TEST_SRC_DIR = src/test/java
BUILD_DIR = $(TARGET_DIR)/classes
TEST_BUILD_DIR = $(TARGET_DIR)/test-classes
JAR_FILE = $(TARGET_DIR)/solution.jar

SOURCES = $(shell find $(SRC_DIR) -name "*.java")
TESTS = $(shell find $(TEST_SRC_DIR) -name "*.java")

NODE_COUNT ?= 10
BIT_LENGTH ?= 10

$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(dir $@)
	$(JAVAC) $(JAVA_FLAGS) -d $(BUILD_DIR) -cp $(BUILD_DIR):$(SRC_DIR) $<

$(TEST_BUILD_DIR)/%.class: $(TEST_SRC_DIR)/%.java
	@mkdir -p $(dir $@)
	$(JAVAC) $(JAVA_FLAGS) -d $(TEST_BUILD_DIR) -cp $(BUILD_DIR):$(SRC_DIR):$(TEST_SRC_DIR) $<

build: $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SOURCES))
	
test-build: $(patsubst $(TEST_SRC_DIR)/%.java,$(TEST_BUILD_DIR)/%.class,$(TESTS))

tests: test-build
	@for test_class in $(shell find $(TEST_BUILD_DIR) -name "*Test.class" | sed 's|$(TEST_BUILD_DIR)/||;s|\.class||' | tr '/' '.'); do \
		echo "Running $$test_class..."; \
		java -cp $(BUILD_DIR):$(TEST_BUILD_DIR) $$test_class || { echo "$$test_class failed"; exit 1; }; \
	done
	@echo "All tests passed."

jar: build
	jar cfe $(JAR_FILE) com.ass3.Simulator -C $(BUILD_DIR) .

clean:
	rm -rf $(BUILD_DIR) $(TEST_BUILD_DIR)

run: jar
	java -jar $(JAR_FILE) $(NODE_COUNT) $(BIT_LENGTH)

rerun: clean all run

case1:
	make run NODE_COUNT=10 BIT_LENGTH=10

case2:
	make run NODE_COUNT=100 BIT_LENGTH=20

case3:
	make run NODE_COUNT=1000 BIT_LENGTH=20

sim: $(JAR_FILE)
	mkdir -p $(LOG_DIR)
	make run case1 | tee $(LOG_DIR)/sim-10_10.log
	make run case2 | tee $(LOG_DIR)/sim-100_20.log
	make run case3 | tee $(LOG_DIR)/sim-1000_20.log

all: clean build jar
