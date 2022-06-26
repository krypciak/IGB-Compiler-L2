IGB Compiler L2 compiles **IGB L2 code** into [**IGB L1 code**](https://github.com/krypciak/IGB-Compiler-L1).  

### IGB L2 is a C-like "high" level language with support for:
- for and while loops (including break; continue;)
- else if statements
- automaticly allocated variables
- arrays
- functions (including returning values and function arguments)
- floating point values up to 3 decimal places (0.001)
- complex equation solving (`var d = a*b/c+1;` works just fine)
- an built-in easy to draw canvas
- easy keyboard input checking
- multi-file support
- sqrt

### Here is a list of stuff the language does NOT support:
- objects
- strings, lists, maps, etc.
- file IO
- dynamic variable allocation (more explained [here]())
- variables in recursion
- switch statements
- dynamic array size (array size has to be known at compile-time)
- there is only one value type (float), so no int casting

<br />

The whole IGB language was designed to be executed in Minecraft ([MCMPCv7](https://github.com/krypciak/MCMPCv7)).  
You can also run it on your desktop using [IGB VM](https://github.com/krypciak/IGB-VM) created for easier program testing.  


## Hello world wrriten in IGB L2
This code displays the string "Hello world!" on the canvas.
```javascript
$ramcell = 70;
$startLine = 15000;

$import <charlib>;

void main() {
  width = 100;
  height = 30;
  stype = rgb;
  
  drawstring(0, 0, "Hello world!", "big", 0);
}
```

## Line by line explanation  
```javascript
$ramcell = 70;
$startLine = 15000;
```
What the hell are those $ things?  
They are called "compiler variables", and are used by the compiler.  
They cannot be accessed in any way in the rest of your code.  
You can set them only once.  


### Here's a list of compiler variables:
- `$startline` (required) This where your code is meant to be placed.  
Startlines are only allowed to be >= 15000, since [charLib](https://github.com/krypciak/IGB-charLib) is taking all the previous space.
- `$ramcell` (required) This where all your variables and arrays will start allocating.  
Ramcells are only allowed to be >= 70, since the previous space is occupied by important variables (see a list of them [here](https://github.com/krypciak/IGB-Compiler-L1#memory-cells)).
- `$lenlimit` (optional) Specifies the maxium instruction size your code can be.  
If your code after compilation to IGB L1 is larger than this number, an error is thrown.
- `$ramlimit` (optional) Same as `$lenlimit` but for variables/arrays.
- `$thread` (optional, default=0) It's working but has no use since threads aren't implemented yet (and probably won't be).  
Diffrent temporary cells for variable and if statement solving are chosen for values 0 or 1.  

___

### Defining variables
It's not in the Hello World, but some things are important to understand.  
Here's a simple variable declaration with initial value:
```javascript
var a = 5;
```
You don't need to provide a value:
```javascript
var a;
```  
**IMPORTANT!** the `| |` thingy specifies a cell.  
`a|70|` means that `a` is going is at cell `70`

Example:
```javascript
var a|75| = 5;
```
However, the code above didn't allocate any memory. Here's how you can allocate it:
```javascript
var a|!75| = 5;
```
If you use the `!` sign with a value outside of your allocation array (from `$ramcell` to `$ramcell` + `$ramlimit`) the compiler will cry.

The same with arrays.
```javascript
var[][] arr1 = new var[7][4];
var[][][] arr2|!80| = new var[16][16];
```

More about variables [here NOOOOOOOOOOO]().

___

### Dependencies
```javascript
$import <charlib>;
```
That's not a compiler variable, that's a dependency.  
For example, the code above imported library named [charLib](https://github.com/krypciak/IGB-charLib), which draws chars on the screen.  
If you import a file/library, all of it's functions are shared and can be used as if they were in the same file.  
Here are some examples:
```
$import myfile // it's the same as the line below
$import myfile.igb_l2 
$import mydirectory/myfile1
$import /home/krypek/myfile2 // it's the same as the line below
$import /home/krypek/myfile2.igb_l2
```  

<br />

___

### Functions

```javascript
void main() {
```
Defines the main function. There can only be 1 main function in a file, it has to return void and it cannot have any arguments.  
To define a function with a return type, replece `void` with `var`
```javascript
var myfuncthatreturns() {
```
Add arguments to the function:
```javascript
var myfuncthatreturns(var a) {
```
You can have multiple functions with the same name with diffrent amount of arguments:
```javascript
var myfuncthatreturns() {}
var myfuncthatreturns(var a) {}
void myfuncthatreturns(var a, var b) {}
```
You don't need to type `return;` at the end of every function, also it the compiler doesn't cry if you don't return any value at all (100% feature).  

<br />

___

### Built-in variables

```javascript
width = 100;
height = 30;
stype = rgb;
```
Those are built-in variables. They work like a normal variables.  
##### Here's a list of them:
- `width`|1| Screen width
- `height`|2| Screen height
- `stype` |3| Can be only set to `16c` or `rgb`.  
Setting it will resize on `width` and `height`, set it's type based on `stype` and fill with #FFFFFF.
- `keyboard` |4| Keyboard input  

<br />

___

#### drawstring()
```javascript
drawstring(0, 0, "Hello world!", "big", 0);
```
Here a compiler function is called.  
Compiler functions can have arguments like strings since they are processed at compile-time.  
For this function to work [charLib](https://github.com/krypciak/IGB-charLib) has to be imported.  
The first 2 arguments specify the x & y of the text.  
Third argument is text, the forth is what type of text is it.  
#### Here's a list of text types:
- `small` (plain, size=14)
- `smallitalic` (italic, size=14)
- `big` (plain, size=20)
- `bigbold` (bold, size=20)
- `bigitalic` (italic, size=20)
- `bigbolditalic` (bold & italic, size=20)

The fifth argument is the text spacing. &nbsp;Y o u r &nbsp;t e x t &nbsp;c a n &nbsp;b e &nbsp;l i k e &nbsp;t h i s .

___

## Here's a list of all compiler functions:
(If the variable type is `int`, only ints can be provided, same with `string`)  
(If the variable type is `cell`, only variables or integers can be provided, no equations)
- `exit()` Jumps to cell -2
- `wait(int tick)` Waits `tick` ticks
- `screenUpdate()` Updates screen properties based on cells [1, 2 and 3](https://github.com/krypciak/IGB-Compiler-L1#memory-cells)
- `dlog(var value)` Logs `value` to the terminal/chat
- [`drawstring(var x, var y, string text, string type, var spacing)`](https://github.com/krypciak/IGB-Compiler-L2/edit/main/README.md#drawstring)
- `sqrt(var variable)` Returns the square root of `variable`
- `random(int from, int to)` Returns a random integer in range of `from` and `to`
- `pixelcache(var cache)` Sets the [pixel cache](https://github.com/krypciak/IGB-VM/blob/main/README.md#pixel-cache) to `cache`
- `pixelcache(var r, var g, var b)` Sets the pixel cache to `r`, `g`, `b`
- `setpixel(var x, var y)` Sets the pixel color at `x` `y` to pixel cache.
- `getpixel(var x, var y)` Returns the [16c](https://github.com/krypciak/IGB-VM/edit/main/README.md#screen-types) pixel type at `x` `y`
- `getpixel(var x, var y, cell r, cell g, cell b)` Saves the rgb value to cells `r`, `g` and `b` at `x` `y`

See theirs implementation [here](/src/me/krypek/igb/cl2/Functions.java).

That's the hello world!

___

## Variable operations and equations  
Here's a list of supported math operations:
- `+`
- `-`
- `*`
- `/`
- `%`

Examples:
```javascript
var a = 2;
var b = 3;

a++; // a is now 3
b--; // b is now 2

a += b; // a is now 5

b = a * b + b / a;
//    10  +  0.4
// b is now 10.4

b -= b % 1; // manual int casting, b is now 10
```

