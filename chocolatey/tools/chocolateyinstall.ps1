$ErrorActionPreference = 'Stop';
$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url64      = 'https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.2.0/boundingboxeditor-2.2.0.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  url64bit      = $url64
  softwareName  = 'boundingboxeditor*'
  checksum64    = '7D61593EBA0BB90E12931E031670B7793026B0AAA46C701D89F4281C658EFAC1'
  checksumType64= 'sha256'
  silentArgs    = '/quiet'
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs