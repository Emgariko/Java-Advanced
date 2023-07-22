#!/bin/bash
if [[ "$#" -eq 2 ]]; then
    java -jar Implementor.jar $1 $2
else 
    if [[ "$#" -eq 3 ]]; then
        java -jar Implementor.jar $1 $2 $3
    else
        echo "Illegal number of arguments"
    fi
fi
