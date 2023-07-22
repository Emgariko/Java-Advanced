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


mkdir build 2> /dev/null
javac -d ./build \
    --module-path ../../java-advanced-2021/artifacts/:../../java-advanced-2021/lib/ \
    --module-source-path ./module \
    --module info.kgeorgiy.ja.garipov.implementor
jar --create --file=Implementor.jar \
    --manifest=MANIFEST.MF \
    -C build/info.kgeorgiy.ja.garipov.implementor .

rm -r ./module
rm -r ./build

