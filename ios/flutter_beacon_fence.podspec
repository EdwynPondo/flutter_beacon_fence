#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_beacon_fence.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_beacon_fence'
  s.version          = '1.0.0'
  s.summary          = 'iOS implementation for the Flutter flutter_beacon_fence plugin.'
  s.description      = <<-DESC
Battery efficient Flutter Beacon fencing that uses native iOS and Android APIs.
                       DESC
  s.homepage      = 'https://github.com/EdwynPondo/flutter_beacon_fence'
  s.license          = { :file => '../LICENSE', :type => 'MIT' }
  s.author           = { 'Edwin Zambrano Nemegyei' => 'edwinzn9@gmail.com' }
  s.source           = { :path => '.', :git => 'https://github.com/EdwynPondo/flutter_beacon_fence.git' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '14.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # See: https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
  s.resource_bundles = {'flutter_beacon_fence' => ['Resources/PrivacyInfo.xcprivacy']}
end
