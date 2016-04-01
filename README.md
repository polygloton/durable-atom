# durable-atom

A Clojure library providing a disk-backed, durable atom implementation

## Usage

With Leiningen:

[![Clojars Project](https://img.shields.io/clojars/v/durable-atom.svg)](https://clojars.org/durable-atom)

It works like an atom, but instead of an initial value you give it a file path.

```clojure
(require '[durable-atom.core :refer [durable-atom]])
(def a (durable-atom "/some/file/path.edn"))
(reset! a {:foo "bar"})
```

## License

Copyright © 2016 Stephen Sloan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
