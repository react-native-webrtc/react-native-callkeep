require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = "RNCallKit"
  s.version             = "1.1.3"
  s.summary             = package['description']
  s.homepage            = "https://github.com/ianlin/react-native-callkit"
  s.license             = "ISC"
  s.author              = "ianlin"
  s.source              = { :git => package['repository']['url'], :tag => "v#{s.version}" }
  s.requires_arc        = true
  s.platform            = :ios, "8.0"
  s.source_files        = "ios/RNCallKit/*.{h,m}"
  s.dependency 'React/Core'
end

