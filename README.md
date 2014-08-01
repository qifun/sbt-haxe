# sbt-haxe

**sbt-haxe** is a [Sbt](http://www.scala-sbt.org/) plugin to compile [Haxe](http://www.haxe.org/) sources in Java/Scala projects.

## Usage

### Step 1: Install `sbt-haxe` into your project

Add the following line to your `project/plugins.sbt`:

    addSbtPlugin("com.qifun" % "sbt-haxe" % "1.0.0")

And add `haxeSettings` to your `build.sbt`:

    haxeSettings

### Step 2: Put your Haxe sources at `src/haxe/yourPackage/YourHaxeClass.hx`

``` haxe
package yourPackage;
import haxe.ds.Vector;
class YourHaxeClass
{
  public static function main(args:Vector<String>)
  {
    trace("Hello, World!");
  }
}
```

### Step 3: Run it!

```
$ sbt run
[info] Loading global plugins from C:\Users\user\.sbt\0.13\plugins
[info] Loading project definition from D:\Documents\sbt-haxe-test\project
[info] Set current project to sbt-haxe-test (in build file:/D:/Documents/sbt-haxe-test/)
[info] "haxe" "-cp" "D:\Documents\sbt-haxe-test\src\haxe" "-cp" "D:\Documents\sbt-haxe-test\target\scala-2.10\src_managed\haxe" "-java-lib" "C:\Users\user\.sbt\boot\scala-2.10.3\lib\scala-library.jar" "-java" "D:\cygwin\tmp\sbt_97a26bd9" "-D" "no-compilation" "yourPackage.YourHaxeClass"
[info] Compiling 1 Java source to D:\Documents\sbt-haxe-test\target\scala-2.10\classes...
[info] Running yourPackage.YourHaxeClass
YourHaxeClass.hx:7: Hello, World!
[success] Total time: 1 s, completed 2014-7-25 10:00:23
```

## Tasks and settings

`sbt-haxe` provides following tasks and settings:

 * haxe
 * dox
 * haxeCommand
 * haxelibCommand
 * doxPlatforms

See [src/main/scala/com/qifun/sbtHaxe/HaxePlugin.scala](https://bitbucket.org/qforce/sbt-haxe/src/master/src/main/scala/com/qifun/sbtHaxe/HaxePlugin.scala#cl-34) for more information.

## Dependencies

`sbt-haxe` requires Sbt 0.13, Haxe 3.1, [hxjava](http://lib.haxe.org/p/hxjava) 3.1.0 and [Dox](http://lib.haxe.org/p/dox) 1.0.0.