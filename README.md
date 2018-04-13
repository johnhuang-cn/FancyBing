[![release 1.0.0](https://img.shields.io/badge/release-1.0.0-blue.svg)](https://github.com/johnhuang-cn/FancyBing)
[![Framework Deeplearning4j](https://img.shields.io/badge/framework-DeepLearning4j-brightgreen.svg)](https://deeplearning4j.org/)
![license GPL](https://img.shields.io/badge/license-GPL-blue.svg)
![language java](https://img.shields.io/badge/language-java-brightgreen.svg)

# FancyBing
A Go program implemented by pure JAVA with Deeplearning4j. The network architecture base on "[Mastering the Game of Go without Human Knowledge](https://deepmind.com/documents/119/agz_unformatted_nature.pdf)", but smaller netowrk, fewer features and without self playing. The network is trained with 1500,000 human games. Then also mixed 300,000 newest leelazero self play games.

# Performance
Tencent Fox 9d (18cores + 1 GTX1080, 15s), about half lost games are vs other AIs or because of misclick. It still not enough strong than Leela Zero, can compete with zen 9d, but less stable.

![FancyBing Fox 9d](docs/images/fancybing.png)

# What Improvements I did
## Improve the ladder reading ability by oversampling
Why most Go bots can't read ladder correctly is because there are few ladder failed samples in normal games. In high dan level games, player would avoid failure ladder in advance. So if the policynetwork is trained with human games, the network can't learn ladder well. Trained with self play games it would be better, but still need long time evolution.

I extract 500,000 continue atari moves (most of them are ladder related) from leela zero selfplay games, mixed them into normal train data which percentage is about 1-2%, after about 200,00 steps (batch: 128) extra training, the ladder reading improve obviously, it can solve most ladder problem after 5,000-20,000 playouts.

## Improve the ko ability by oversampling
The method is same as above, but the ko moves are extracted from high level games.

## Improve opening
After normal training, I trained opening network with only 0-100 moves, the accuracy and MSE improved. When playing moves 0-80, use the opening netowrk, and then using the normal netowrk, the perofmrnace up to Fox 9d from weaker 8d.

I also trained mid game netowrk with 100+ moves, and end game network with 180+ moves, but seems not obviously improved. I didn't fully compare them.

# Requirements
* Recommend 8G+ Memory
* Nvidia GPU & Driver installed
* [CUDA 8.0](https://developer.nvidia.com/cuda-zone)
* [CUDNN 6.0](https://developer.nvidia.com/cudnn)
* [JDK1.8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [GoGUI](https://sourceforge.net/projects/gogui/)

# Usage
At first, I don't recommend normal Go fans use any Go bots as a plug-in to play games in opening platforms such like KGS, Fox, Tygem. It is less interesting for other players to play too many games with computer. If the AI developer needs test it for researching, I suggest you mark AI in ID description. So that the human player can accept or reject to play against AI.

So, I didn't simply the requirements and usages steps here. But I think such are easy for true AI developers.

## Windows
* Finish the requiements section
* Download package and extract it
* Run startPolicyNetService.bat
* Run startFancybingService.bat
* Attach FancyBing player to GoGui, open GoGUI > Program > New Program<br/>Command: java -jar fancybing-gtp-1.0.jar<br/>Working Directory: the path of Fancybing<br/>
![Attach to GoGUI](/docs/images/attach_to_gogui.png)

# Training
## Download SGFs
[Computer go database](https://github.com/yenw/computer-go-dataset)
Please transfer other format into sepearte SGF files.

## Generate the training data
See [FeatureGenerator.java](/fancybing-train/src/main/java/net/xdevelop/go/preprocess/FeatureGenerator.java)

The generator would random pick moves from the sgfs and generate feature files named 0.txt, 1.txt, 2.txt... each contains 51200 records.

Please use the all() function to generate normal training data. The open(), mid(), end() functions are used to generate data for opening, mid, end game.

## Training
See [ResNetwork.java](/fancybing-policynet/src/main/java/net/xdevelop/go/policynet/PolicyNetService.java)

The early stop implementation is not good enough in DL4j 0.9.1, so I train the model by sepearate data file, so that you can stop the training at any time, and adjust the learing rate then continue the training by manual increase the start file index.

# License
The code is released under the GPLv3 or later, any commercial usage please contact me (john.h.cn@gmail.com).
