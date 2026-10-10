cask "boundingboxeditor" do
  # The Apple Silicon disk image has no suffix, the Intel one "-intel".
  arch intel: "-intel"

  version "__VERSION__"
  sha256 arm:   "__SHA256_ARM__",
         intel: "__SHA256_INTEL__"

  url "https://github.com/mfl28/BoundingBoxEditor/releases/download/v#{version}/boundingboxeditor-#{version}#{arch}.dmg"
  name "Bounding Box Editor"
  desc "Image annotation tool for bounding boxes, polygons and masks"
  homepage "https://github.com/mfl28/BoundingBoxEditor"

  livecheck do
    url :url
    strategy :github_latest
  end

  depends_on :macos

  app "BoundingBoxEditor.app"

  # The app is not notarized, so Gatekeeper would refuse to open it while it is quarantined.
  postflight_steps do
    run "/usr/bin/xattr", args: ["-dr", "com.apple.quarantine", "{{appdir}}/BoundingBoxEditor.app"]
  end

  caveats <<~EOS
    Bounding Box Editor is not notarized by Apple. This cask removes the quarantine
    attribute from the installed app so that it can be opened.
  EOS
end
