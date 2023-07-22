mkdir module
mkdir build

set modulename="info.kgeorgiy.ja.garipov.implementor"
mkdir module\%modulename%\info\kgeorgiy\ja\garipov\implementor

cd ..
cd java-solutions\info\kgeorgiy\ja\garipov\implementor
Xcopy Implementor.java ..\..\..\..\..\..\script\module\%modulename%\info\kgeorgiy\ja\garipov\implementor
cd ..\..\..\..\..\..\script
Xcopy module-info.java "module/%modulename%"

javac -d ./build ^
 --module-path ..\..\java-advanced-2021\artifacts\;..\..\java-advanced-2021\lib\  ^
 --module-source-path .\module ^
 --module info.kgeorgiy.ja.garipov.implementor

jar --create --file=Implementor.jar ^
    --manifest=MANIFEST.MF ^
    -C build/info.kgeorgiy.ja.garipov.implementor .

rd /S /Q build
rd /S /Q module

