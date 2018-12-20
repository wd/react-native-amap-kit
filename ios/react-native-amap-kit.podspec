require 'json'

package = JSON.parse(File.read(File.join(__dir__, '../package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']

  s.authors      = { package['author'] => '' }
  s.homepage     = package['repository']['url']
  s.license      = package['license']
  s.platform     = :ios, "9.0"

  s.source       = { :git => package['repository']['url'] }

  s.dependency 'AMap3DMap', '5.7.0'
  s.dependency 'AMapLocation', '2.6.0'
  s.dependency 'AMapSearch', '5.7.0'
end
