# java-apk-obfuscator
APK black-box obfuscator for my Master's project. Written 100% in Java and inspired by
[Obfuscapk](https://github.com/ClaudiuGeorgiu/Obfuscapk).

## Obfuscators
### Const-string
> Convert strings to method calls that return the 
> original string through byte manipulation
> 
### Const
> Add redundant math to const ints to hide from global search: 10 = 14 - 4

### Debug Removal
> Remove debug lines and information

### Field Rename
> Rename fields

### Method Rename
> Rename methods

### Method Overloading
> Multiple methods have the same name with different parameters

### Class Rename
> Rename classes and packages in all the source code and resource files

### Resource Value Rename
> Rename values in res/values-* folders (colors, strings, ...)

### Resource File Rename
> Rename drawables and other resource files

## Libraries
* [APKTool](https://github.com/patrickfav/uber-apk-signer) to decompile and build Apks
* [Uber Apk Signer](https://github.com/patrickfav/uber-apk-signer) to sign built Apks


# Want to contribute?
Email me at oscar0812torres@gmail.com, or text me on [Telegram](https://telegram.me/bittle).