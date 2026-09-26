$ErrorActionPreference = 'Stop';
$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url64      = 'https://github.com/mfl28/BoundingBoxEditor/releases/download/v__VERSION__/boundingboxeditor-__VERSION__.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  url64bit      = $url64
  softwareName  = 'boundingboxeditor*'
  checksum64    = '__CHECKSUM__'
  checksumType64= 'sha256'
  silentArgs    = '/quiet'
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs
