import subprocess
import os
from pathlib import Path
import _thread as t
from datetime import datetime

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
    #flags
    # powerGenAgent = {'-p' : 1}
    # powerDisAgent = {'-pd' : 1}
    # smartHomeAgent = {'-sh' : 5}
    # evAgent = {'-ev' : 2}
    # runTime = {'-rt' : 60}
    # waitTime = {'-wt' : 2000}
    powergen = '-pd'
    pd = '1'
    powerdi = '-pd'
    pd = '1'
    smarthome = '-sh'
    sh = '6'
    electronicveh = '-ev'
    ev = '2'
    runtime = '-rt'
    rt = '15'
    wait = '-wt'
    wt = '2000'
    
    indexName = '-indexname'
    now = datetime.now()
    dt_string = now.strftime("smartgrid-%Y-%m-%dt%H.%M.%S.%f")
    # indexName = {'-indexname' : dt_string}
    
    jarfile = getJarFile()
    ret = subprocess.call(['java', '-jar', jarfile, powergen, pd, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string])
    return ret


def main():
    # t.start_new_thread(executeJar, ())
    executeJarRunner()

if __name__ == '__main__':
    main()
