Static init plugin
=============
 Re-executes the static initializer method, if the length of it's body is changed. Note: All the initialization
 expressions of static fields get compiled into the static initializer (<clinit>) block. If you change one
 initializing method call to another, then the length won't change. If you add static field like "int t = 1" then the
 length changes and it is re-executed.