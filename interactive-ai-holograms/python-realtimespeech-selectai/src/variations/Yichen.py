for i in range(2):
        numberOne = float(input("type in first number:" ))
        numberTwo = float(input("type in second number:"))

        if numberOne < numberTwo:
            print("numberONe is less than numberTwo")
        elif numberOne == numberTwo:
            print("numberOne is equal to numberTwo")
        else:
            print("numberTwo is less than numberOne")

        print(numberOne + numberTwo)