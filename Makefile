#Author: Tiger Barras
#Makefile for ASG2 cs455


JC = javac -d .
default: all

all:
	@echo "Compiling all classes. . ."
	$(JC) ./cs455/harvester/*/*.java
	@echo "Cleaning graph file for next run"
	$(RM) /tmp/cs455-wbarras/*/nodes/*/*
