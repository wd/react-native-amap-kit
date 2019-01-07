require 'json'

package = JSON.parse(File.read(File.join(__dir__, './package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']

  s.authors      = { package['author'] => '' }
  s.homepage     = package['repository']['url']
  s.license      = package['license']
  s.platform     = :ios, "9.0"

  s.source       = { :git => package['repository']['url'] }
  s.source_files = "ios/RCTAMap/RCTAMap/*.{h,m}"

  s.dependency 'React'
  s.dependency 'AMap3DMap', '6.6.0'
  s.dependency 'AMapLocation', '2.6.1'
  s.dependency 'AMapSearch', '6.6.0'
end
