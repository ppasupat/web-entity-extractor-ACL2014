default:
	mkdir -p classes
	javac -cp lib/\* -d classes `find src -name "*.java"`

clean:
	rm -rf classes
