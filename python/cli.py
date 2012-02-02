import build

def main():
    dita2xhtml = build.Dita2xhtml()
    build.properties["transtype"] = "xhtml"
    build.properties["args.input"] = "doc/userguide.ditamap"
    dita2xhtml.dita2xhtml()

if __name__ == "__main__":
    main()