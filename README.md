
Based on the paper : https://graphics.stanford.edu/~mdfisher/cloth.html

Using the spring-based approach (Mass-Spring Model)

## Run

The renderer uses LWJGL 3 with OpenGL 3.3. On a Linux x86-64 system, run:

```bash
mvn compile exec:java
```

Press Escape or close the window to stop the simulation.




To try to implement later :

 The Verlet integration algorithm is such an explicit model with the very interesting propety that it does not need to know anything about the velocity; it computes this internally via looking at the position at both the current and previous time step.

Another wonderful aspect of this algorithm is that like 4th order Runge-Kutta, it is 4th order accurate. Because it is quite accurate, easy to implement, and does not need the velocity terms, it is my favorite explicit model and the one I use in all my cloth models. 
