## Metamorphic Relations
  
A metamorphic relation with respect to a function <img src="https://latex.codecogs.com/gif.latex?f"/> is defined as a relation over a sequence of inputs <img src="https://latex.codecogs.com/gif.latex?&#x5C;langle%20x_1,...,%20x_n&#x5C;rangle"/> where <img src="https://latex.codecogs.com/gif.latex?n&#x5C;geq2"/> and their corresponding sequence of outputs <img src="https://latex.codecogs.com/gif.latex?&#x5C;langle%20f(x_1),...f(x_n)&#x5C;rangle"/>, i.e.
  
  
  
### Common items
  
Sequence of Inputs:  <img src="https://latex.codecogs.com/gif.latex?{&#x5C;langle%20x_i%20&#x5C;rangle}_{i=1..n}"/>
Sequence of Outputs: <img src="https://latex.codecogs.com/gif.latex?{&#x5C;langle%20f(x_i)%20&#x5C;rangle}_{i=1..n}"/>
Metamorphic Relation: <img src="https://latex.codecogs.com/gif.latex?R({&#x5C;langle%20f(x_i)%20&#x5C;rangle}_{i=1..n}0"/>
  
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
+ MR1: 
**if**  two different source programs, <img src="https://latex.codecogs.com/gif.latex?P_1"/> and <img src="https://latex.codecogs.com/gif.latex?P_2"/>, are functionally equivalent
**then** their obfuscated versions, <img src="https://latex.codecogs.com/gif.latex?O(P_1)"/> and <img src="https://latex.codecogs.com/gif.latex?O(P_2)"/> should also be functionally  equivalent and, therefore, the compiled obfuscated executable pro- grams, <img src="https://latex.codecogs.com/gif.latex?C(O(P_1))"/> and <img src="https://latex.codecogs.com/gif.latex?C(O(P_2))"/>, should have equivalent behavior, i.e. the same outputs for the same inputs.
  