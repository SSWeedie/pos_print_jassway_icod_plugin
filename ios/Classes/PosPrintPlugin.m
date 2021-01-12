#import "PosPrintPlugin.h"
#if __has_include(<PosPrintPlugin/PosPrintPlugin-Swift.h>)
#import <PosPrintPlugin/PosPrintPlugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "PosPrintPlugin-Swift.h"
#endif

@implementation PosPrintPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPosPrintPlugin registerWithRegistrar:registrar];
}
@end
