require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = "RNCallKeep"
  s.version             = package['version']
  s.summary             = package['description']
  s.homepage            = package['homepage']
  s.license             = package['license']
  s.author              = package['author']
  s.source              = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.requires_arc        = true
  # React Native raises the pod's target to its minimum iOS version after install, but not the
  # privacy resource bundle target, which Xcode 27 rejects below iOS 15.
  s.platform            = :ios, (defined?(min_ios_version_supported) ? min_ios_version_supported : "8.0")
  s.source_files        = "ios/RNCallKeep/*.{h,m}"
  s.dependency 'React'
  # Lets a Swift AppDelegate or SceneDelegate `import RNCallKeep` without a bridging header.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES' }
  s.resource_bundles = {
    'RNCallKeepPrivacyInfo' => ['ios/PrivacyInfo.xcprivacy'],
  }
end

