---
layout: default
---

## Experimenting with typekey in the Scala REPL

Here's a quick formula for experimenting with typekey in the REPL (Scala's interactive shell). I'm
going to start it up inside SBT in the emblem project, but this basic procedure should work for any
project that [declares a dependency on typekey](libdep.html).

Download the source code for emblem:

    git clone https://github.com/longevityframework/typekey.git

Enter the directory created by `git`:

    cd typekey

Now boot into SBT:

    sbt

Boot up the REPL:

    console

This will give you a `scala>` prompt.  Import some utility classes like so:

    import typekey.typeKey

Start playing around!

    println(typeKey[List[String]])
