## CursiveParser-ServerSide

This program takes as input a picture of a written text then starts processing it into a .docx.
It does this by:
- grayscaling the image
- creates a histeogram to detect rows of text
- creates another histeogram for every row and word in the text with the goal to sepparate letters
- passes the images of letters trough a neural network to rebuild the text into an electronic format

## Photos
- Original image

![snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236509414-2a250543-dd82-48e8-b47a-07ab64cbd6cd.png)

- Histeogram for detecting rows

![PushedLeftCrude_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236509777-cb1523a5-d410-4c35-85a3-dd91291de80e.png)

![Screenshot_1](https://user-images.githubusercontent.com/127097933/236510192-b72c3403-c5ce-456b-8bd4-beaf89dfa2a1.png)

- Detected words example

![Word9-6_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510567-4d4d06b3-277b-4a0d-a1f0-edeab1e2a3ef.png)

![Word9-2_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510693-d46c707a-2692-450f-828c-6c6d56ae5f39.png)

- Detected characters example

![Letter95_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510745-a398da7d-0b6d-43f6-bdf5-c525fb666ba0.png)

![Letter715_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510777-aeec0187-7bdc-441a-b918-554e0462ce3d.png)

![Letter709_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510781-ce2157e8-cd11-472f-b59d-9580e7e1c3cf.png)

![Letter50_snapshot20230315131955](https://user-images.githubusercontent.com/127097933/236510887-c36e207e-67ed-41a0-b2a9-7f8af3b12f0a.png)

## Links:
<br><br>
[![Pic](https://imgur.com/7rEwc2X.png)](https://github.com/denisdenis05/CursiveParser-ClientSide)
<br><br>
[![Pic](https://imgur.com/2vKNQCr.png)](https://github.com/LucianuSebi/CursiveParser-WebConnection)
<br><br>
[![Pic](https://imgur.com/RDkIOuU.png)](https://github.com/LucianuSebi/CursiveParser-ServerSide)
<br><br>

<br><br>

## Description:

We are currently training a neural network as the last step of the project's core.

Illustration<br>
![Pic](https://imgur.com/1mwkWjo.png)
