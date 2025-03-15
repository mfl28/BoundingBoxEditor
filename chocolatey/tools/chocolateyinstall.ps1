$ErrorActionPreference = 'Stop';
$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url64      = 'https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.8.0/boundingboxeditor-2.8.0.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  url64bit      = $url64
  softwareName  = 'boundingboxeditor*'
  checksum64    = '93589ADC4DAC0BD436C9C64BA82A73FF87FFB9042C14BFCDDF4C3E3391113DCB'
  checksumType64= 'sha256'
  silentArgs    = '/quiet'
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs
