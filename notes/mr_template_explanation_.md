## Metamorphic Relations

A metamorphic relation with respect to a function <img src="https://latex.codecogs.com/gif.latex?f"/> is defined as a relation over a sequence of inputs <img src="https://latex.codecogs.com/gif.latex?&#x5C;langle&#x5C;:x_1,...,%20x_n&#x5C;:&#x5C;rangle"/> where <img src="https://latex.codecogs.com/gif.latex?n&#x5C;geq2"/> and their corresponding sequence of outputs <img src="https://latex.codecogs.com/gif.latex?&#x5C;langle&#x5C;:f(x_1),...f(x_n)&#x5C;:&#x5C;rangle"/>, i.e.



### Common items

Sequence of Inputs:  <img src="https://latex.codecogs.com/gif.latex?{&#x5C;langle&#x5C;:x_i&#x5C;:&#x5C;rangle}_{i=1..n}"/>
Sequence of Outputs: <img src="https://latex.codecogs.com/gif.latex?{&#x5C;langle&#x5C;:f(x_i)&#x5C;:&#x5C;rangle}_{i=1..n}"/>
Metamorphic Relation: <img src="https://latex.codecogs.com/gif.latex?R(&#x5C;:{&#x5C;langle&#x5C;:f(x_i)&#x5C;:&#x5C;rangle}_{i=1..n}0"/>

<br>
### Template for Metamorphic Relations

**In the domain of** \<application domain>
[**where** \<context definition>]
[**assuming** that \<constraints>]
**the following metamorphic relation(s) should hold**
+ <metamorphic relation <img src="https://latex.codecogs.com/gif.latex?name_1"/>>:
  **if** <relation among inputs/outputs>
  **then** <relation among inputs/outputs>
  ...
+ <metamorphic relation <img src="https://latex.codecogs.com/gif.latex?name_n"/>>:
  **if** <relation among inputs/outputs>
  **then** <relation among inputs/outputs>

<br>
####  Examples (Simple)

**In the domain of** cybersecurity (code obfuscators)
**the following metamorphic relation(s) should hold**
+ MR<img src="https://latex.codecogs.com/gif.latex?_1"/>:
  **if**  two different source programs, <img src="https://latex.codecogs.com/gif.latex?P_1"/> and <img src="https://latex.codecogs.com/gif.latex?P_2"/>, are functionally equivalent
  **then** their obfuscated versions, <img src="https://latex.codecogs.com/gif.latex?O(P_1)"/> and <img src="https://latex.codecogs.com/gif.latex?O(P_2)"/> should also be functionally  equivalent and, therefore, the compiled obfuscated executable pro- grams, <img src="https://latex.codecogs.com/gif.latex?C(O(P_1))"/> and <img src="https://latex.codecogs.com/gif.latex?C(O(P_2))"/>, should have equivalent behavior, i.e. the same outputs for the same inputs.

<br>
####  Examples (Formal)

**In the domain of** cybersecurity(code obfuscators)
**where**
+ <img src="https://latex.codecogs.com/gif.latex?P,&#x5C;,P_1&#x5C;,and&#x5C;,P_2"/> are computer programs.
+ <img src="https://latex.codecogs.com/gif.latex?&#x5C;Omega"/> is a program obfuscation function.
+ <img src="https://latex.codecogs.com/gif.latex?&#x5C;Omega(p)@[t_i]"/> is the obfuscation of <img src="https://latex.codecogs.com/gif.latex?p"/> at time <img src="https://latex.codecogs.com/gif.latex?t_i"/>.
+ <img src="https://latex.codecogs.com/gif.latex?&#x5C;equiv"/> is the program functional equivalence relation.

**the following metamorphic relation(s) should hold.**
+ MR<img src="https://latex.codecogs.com/gif.latex?_1"/>:
  **if** <img src="https://latex.codecogs.com/gif.latex?P_1%20&#x5C;equiv%20P_2"/>, i.e. <img src="https://latex.codecogs.com/gif.latex?P_1"/> and <img src="https://latex.codecogs.com/gif.latex?P_2"/> are functionally equivalent
  **then** <img src="https://latex.codecogs.com/gif.latex?&#x5C;Omega(P_1)%20&#x5C;equiv%20&#x5C;Omega(P_2)"/>, i.e. the obfuscation of <img src="https://latex.codecogs.com/gif.latex?P_1"/> and <img src="https://latex.codecogs.com/gif.latex?P_2"/> are also functionally equivalent.
+ MR<img src="https://latex.codecogs.com/gif.latex?_2"/>:
  **if** <img src="https://latex.codecogs.com/gif.latex?&#x5C;{&#x5C;:t_i&#x5C;:&#x5C;}_{i=1..n}"/> are different times
  **then** <img src="https://latex.codecogs.com/gif.latex?&#x5C;forall%20i%20:%201..n-1%20&#x5C;;&#x5C;;&#x5C;bullet&#x5C;;&#x5C;;%20&#x5C;Omega(p)@[t_{i}]%20&#x5C;equiv%20&#x5C;Omega(p)@[t_{i+1}]"/>, i.e. the obfuscation process does not depend on the obfuscator environment (time of execution in case).
  
  