import subprocess
import os
from pathlib import Path
import _thread as t

def getJarFile():
    base = Path(__file__).parent.parent / "target"
    for file in os.listdir(base):
        if "shaded" in file:
            return base / file

def executeJarRunner():
    ret = executeJar()
    if ret != 0:
        print("failed")
    else:
        print("success")
        executeJar()


def executeJar():
    os.getcwd()
    jarfile = getJarFile()
    ret = subprocess.call(['java', '-jar', jarfile, '-rt', '15'])
    return ret


def main():
    t.start_new_thread(executeJar, ())
    executeJarRunner()



if __name__ == '__main__':
    main()
