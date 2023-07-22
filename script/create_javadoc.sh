#!/bin/bash
modulename='info.kgeorgiy.ja.garipov.implementor'
# make dir ./module
mkdir ./module 2> /dev/null
# copy implementor(dir with the file within) to ./module
cp -r ../java-solutions/info/kgeorgiy/ja/garipov/implementor ./module/
# make module dirs
mkdir -p ./module/$modulename/info/kgeorgiy/ja/garipov
# copy implementor(dir with the file Implementor.java within) to the corresponding dir in the module
cp -r ./module/implementor ./module/$modulename/info/kgeorgiy/ja/garipov
# remove implementor(dir with the file Implementor.java within)
rm -r ./module/implementor
# copy module-info.java
cp module-info.java ./module/$modulename/
cd ../

javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ -private -d docs \
    -cp ../java-advanced-2021/artifacts/info.kgeorgiy.java.advanced.implementor.jar:../java-advanced-2021/lib/quickcheck-0.6.jar:../java-advanced-2021/lib/junit-4.11.jar \
    ./script/module/$modulename/info/kgeorgiy/ja/garipov/implementor/* \
    ../java-advanced-2021/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java \
    ../java-advanced-2021/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java \
    ../java-advanced-2021/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java
    
rm -r ./script/module
