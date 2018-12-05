import os
import ConfigParser
from rdf_walks2vec_app import ConfigSectionReader
from matplotlib import pyplot as plt
Config = ConfigParser.ConfigParser()
Config.read(os.path.dirname(__file__)+"\\config.ini")
#X = ConfigSectionReader(Config,"evaluation_app")['yield'].rstrip('\n').split(",")

with open(ConfigSectionReader(Config,"evaluation_app")['precision_scores_file']) as f:
        content = f.readlines()
        plus_RGB = 0
        for line in content:
            line = line.rstrip()
	    splitted = line.split("\t")
	    X = [0]
	    Y = [0]
	    for splitted_part in splitted:
	       x, y = splitted_part.split(',')
	       X.append(x)
	       Y.append(y)
	    plt.plot(X,Y,'-o', color=(0.1, 0.1,0.1+plus_RGB))
	    plus_RGB = plus_RGB + 0.1
#X=[5,10,20,30,40,60,80,100]
#Y1=[1,3,6,8,10,14,18,20]
#Y2=[2,3,4,7,9,14,17,22]
#Y3=[0,4,6,10,14,20,26,29]
#plt.plot(X,Y1,'-o', color=(0.1,0,0))
#plt.plot(X,Y2,'-o',color=(0.5,0,0))
#plt.plot(X,Y3,'-o',color=(0.9,0,1))
plt.show()