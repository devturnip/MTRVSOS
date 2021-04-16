import subprocess

def executeJarRunner():
    ret = executeJar()
    if ret !=0:
        print("failed")
    else:
        print("success")

def executeJar():
    jarFile = '/Users/tony/Google Drive/School/KAIST/Lessons/SELAB/Projects/MTRVSOS/target/MTRVSOS-1.0-SNAPSHOT-shaded.jar'
    ret = subprocess.call(['java', '-jar', jarFile, '-rt', '15'])
    return ret

def main():
    executeJar()

if __name__ == '__main__':
    main()
    