# sbt-haxe

**sbt-haxe** is a [Sbt](http://www.scala-sbt.org/) plugin to compile [Haxe](http://www.haxe.org/) sources in Java/Scala projects.

## Usage

### Step 1: Install `sbt-haxe` into your project

Add the following line to your `project/plugins.sbt`:

    addSbtPlugin("com.qifun" % "sbt-haxe" % "1.0.0")

And add `pttrtSettings` to your `build.sbt`:

    haxeSettings

### Step 2: Put your Haxe sources at `src/haxe/yourPackage/YourHaxeClass.hx`

``` haxe
package yourPackage;
class YourHaxeClass
{
  public static function main()
  {
    trace("Hello, World!");
  }
}
```

### Step 3: Start Sbt

```
$ sbt
> 
```

### Step 4: Compile and run it!

```
> compile
[info] Updating {file:/D:/Documents/sbt-haxe-test/}sbt-haxe-test...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] "haxe" "-cp" "D:\Documents\sbt-haxe-test\src\haxe" "-cp" "D:\Documents\sbt-haxe-test\target\scala-2.10\src_managed\haxe" "-java-lib" "C:\Users\user\.sbt\boot\scala-2.10.3\lib\scala-library.jar" "-java" "D:\cygwin\tmp\sbt_71654c88" "-D" "no-compilation" "yourPackage.YourHaxeClass"
[info] Compiling 29 Java sources to D:\Documents\sbt-haxe-test\target\scala-2.10\classes...
[success] Total time: 2 s, completed 2014-7-25 9:23:25
> console
[info] Starting scala interpreter...
[info]
Welcome to Scala version 2.10.3 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_45).
Type in expressions to have them evaluated.
Type :help for more information.

scala> yourPackage.YourHaxeClass.main()
YourHaxeClass.hx:6: Hello, World!
```

## Tasks and settings

`sbt-haxe` provides following tasks and settings:

 * Tasks
   * haxe
   * dox
 * Settings
   * haxeCommand
   * haxelibCommand
   * doxPlatforms

See [src/main/scala/com/qifun/sbtHaxe/HaxePlugin.scala](https://bitbucket.org/qforce/sbt-haxe/src/master/src/main/scala/com/qifun/sbtHaxe/HaxePlugin.scala#cl-34) for more information.

## Dependencies

`sbt-haxe` requires Sbt 0.13, Haxe 3.1, [hxjava](http://lib.haxe.org/p/hxjava) 3.1.0 and [Dox](http://lib.haxe.org/p/dox) 1.0.0.