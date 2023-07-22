mkdir module
mkdir build

set modulename="info.kgeorgiy.ja.garipov.implementor"
mkdir module\%modulename%\info\kgeorgiy\ja\garipov\implementor

cd ..
cd java-solutions\info\kgeorgiy\ja\garipov\implementor
Xcopy Implementor.java ..\..\..\..\..\..\script\module\%modulename%\info\kgeorgiy\ja\garipov\implementor
Xcopy package-info.java ..\..\..\..\..\..\script\module\%modulename%\info\kgeorgiy\ja\garipov\implementor
cd ..\..\..\..\..\..\script
Xcopy module-info.java "module/%modulename%"


cd ..
javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ -private -d docs  -cp ..\..\java-advanced-2021\artifacts\info.kgeorgiy.java.advanced.implementor.jar: script\module\info.kgeorgiy.ja.garipov.implementor\info\kgeorgiy\ja\garipov\implementor\Implementor.java ..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\Impler.java ..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\JarImpler.java ..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\ImplerException.java

cd script

rd /S /Q build

rd /S /Q module

